"""Integration checks against a running dev API; creates and cleans isolated fixtures."""

import json
import os
import sys
import urllib.error
import urllib.request
import uuid


def verify(base_url):
    marker = "QA-" + uuid.uuid4().hex[:12]
    fixtures = []
    expedients = []
    graduate_ids = set()
    headers = {"Content-Type": "application/json"}
    token = os.environ.get("API_TOKEN")
    if token:
        headers["Authorization"] = "Bearer " + token

    def call(method, path, payload=None, expected=200):
        data = None if payload is None else json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(base_url + path, data, headers, method=method)
        try:
            response = urllib.request.urlopen(request, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        body = response.read().decode("utf-8")
        if response.code != expected:
            raise AssertionError(f"{method} {path}: expected {expected}, got {response.code}: {body}")
        return json.loads(body) if body else None

    def fixture(path, payload):
        result = call("POST", path, payload, 201)
        fixtures.append((path, result["id"]))
        return result["id"]

    try:
        school = fixture("/schools", {"name": marker + " School"})
        modality = fixture("/degree-modalities", {"name": marker + " Thesis", "active": True})
        status_name = marker + " Observado"
        status = fixture("/expedient-statuses", {"name": status_name, "active": True})
        teachers = [fixture("/teachers", {"fullName": marker + f" Teacher {i}"}) for i in range(3)]
        for catalog, identifier in [
            ("schools", school), ("degree-modalities", modality),
            ("expedient-statuses", status), ("teachers", teachers[0])
        ]:
            assert any(item["id"] == identifier for item in call("GET", "/catalogs/" + catalog))

        number = marker + "-001"
        payload = {
            "number": number, "startDate": "2026-03-15", "degree": "Título profesional",
            "modalityId": modality, "statusId": status, "observations": "Ingreso inicial",
            "graduate": {"fullName": "Sofia Valdivia Paredes", "documentNumber": marker,
                         "studentCode": marker + "-ST", "schoolId": school, "academicProgram": "Pregrado"},
            "research": {"title": "Arquitectura de microservicios"}
        }
        initial = call("POST", "/expedients", payload, 201)
        expedients.append(number)
        graduate_id = initial["graduate"]["id"]
        graduate_ids.add(graduate_id)
        assert initial["degree"] == payload["degree"]
        assert initial["observations"] == "Ingreso inicial"
        assert initial["graduate"]["studentCode"] == marker + "-ST"
        research_id = initial["research"]["id"]
        call("POST", "/expedients", payload, 409)
        assert call("GET", "/expedients/" + number.lower())["graduate"]["fullName"] == "Sofia Valdivia Paredes"

        second_payload = {**payload, "number": marker + "-002"}
        second = call("POST", "/expedients", second_payload, 201)
        expedients.append(second_payload["number"])
        assert second["graduate"]["id"] == graduate_id, "Existing graduate must be reused"
        page = call("GET", f"/expedients?search={marker}&schoolId={school}&statusId={status}&modalityId={modality}&year=2026&page=0&size=1")
        assert page["totalElements"] == 2 and page["totalPages"] == 2 and len(page["content"]) == 1
        next_page = call("GET", f"/expedients?search={marker}&page=1&size=1")
        assert next_page["content"][0]["number"] != page["content"][0]["number"]
        assert call("GET", f"/expedients?search={marker}&year=2025")["totalElements"] == 0
        call("GET", "/expedients?page=-1", expected=400)
        call("GET", "/expedients?size=0", expected=400)

        bad_number = marker + "-BAD"
        bad_payload = {**payload, "number": bad_number,
                       "graduate": {**payload["graduate"], "documentNumber": marker + "-BAD", "studentCode": marker + "-BAD", "schoolId": 9223372036854775807}}
        call("POST", "/expedients", bad_payload, 404)
        call("GET", "/expedients/" + bad_number, expected=404)
        assert not any(g["documentNumber"] == marker + "-BAD" for g in call("GET", "/graduates")), "Failed aggregate creation must roll back"

        payload["research"]["title"] = "Trabajo actualizado"
        payload["graduate"]["fullName"] = "Sofia Valdivia Paredes Actualizada"
        updated = call("PUT", "/expedients/" + number, payload)
        assert updated["research"]["id"] == research_id and updated["research"]["title"] == "Trabajo actualizado"
        assert updated["graduate"]["id"] == graduate_id
        assert updated["graduate"]["fullName"] == payload["graduate"]["fullName"]
        state = call("PATCH", f"/expedients/{number}/status", {"status": status_name.lower(), "observations": "Falta constancia"})
        assert state["observations"] == "Falta constancia" and state["summary"]["statusId"] == status

        advisor_request = {"teacherId": teachers[0], "resolutionNumber": marker + "-AS", "assignedAt": "2026-04-10"}
        advisor = call("PUT", f"/expedients/{number}/advisor", advisor_request)["advisor"]
        advisor_request["teacherId"] = teachers[1]
        advisor_update = call("PUT", f"/expedients/{number}/advisor", advisor_request)["advisor"]
        assert advisor_update["id"] == advisor["id"] and advisor_update["resolutionId"] == advisor["resolutionId"]
        assert advisor_update["teacherId"] == teachers[1]

        jury_request = {"resolutionNumber": marker + "-JR", "members": [
            {"teacherId": teacher, "role": role} for teacher, role in zip(teachers, ["Presidente", "Secretario", "Vocal"])]}
        jury = call("PUT", f"/expedients/{number}/jury", jury_request)
        jury_request["members"][0]["role"], jury_request["members"][1]["role"] = "Secretario", "Presidente"
        jury = call("PUT", f"/expedients/{number}/jury", jury_request)
        assert len(jury["jury"]) == 3
        assert len({member["resolutionId"] for member in jury["jury"]}) == 1
        bad_jury = {"resolutionNumber": marker + "-INVALID", "members": [dict(m) for m in jury_request["members"]]}
        bad_jury["members"][1]["teacherId"] = teachers[0]
        call("PUT", f"/expedients/{number}/jury", bad_jury, 400)
        bad_jury["members"][1]["teacherId"] = 9223372036854775807
        call("PUT", f"/expedients/{number}/jury", bad_jury, 404)
        unchanged = call("GET", "/expedients/" + number)
        assert unchanged["jury"] == jury["jury"], "Rejected jury update must preserve existing assignments"
        assert all(r["number"] != marker + "-INVALID" for r in unchanged["resolutions"])

        draw_request = {"drawDate": "2026-05-02", "resolutionNumber": marker + "-DR", "teacherIds": teachers}
        draw = call("POST", f"/expedients/{number}/draw", draw_request, 201)
        assert len(draw["draws"]) == 1 and len(draw["draws"][0]["teachers"]) == 3
        draw_request["teacherIds"] = [teachers[0], teachers[0]]
        call("POST", f"/expedients/{number}/draw", draw_request, 400)

        defense = call("PUT", f"/expedients/{number}/defense", {"defenseDate": "2026-06-15", "result": "Aprobado"})["defense"]
        defense_update = call("PUT", f"/expedients/{number}/defense", {"defenseDate": "2026-06-16", "result": "Aprobado por unanimidad"})["defense"]
        assert defense["id"] == defense_update["id"] and defense_update["date"] == "2026-06-16"
        legacy = call("GET", f"/expedients/{initial['summary']['id']}/detail")
        assert legacy["summary"]["number"] == number
        assert len(call("GET", f"/expedients/search?search={marker}")) == 2

        for expedient in list(expedients):
            call("DELETE", "/expedients/" + expedient, expected=204)
            expedients.remove(expedient)
            call("GET", "/expedients/" + expedient, expected=404)
        print("PASS: aggregate CRUD, pagination, identity reuse, validation, rollback, catalogs and academic workflow")
    finally:
        for expedient in expedients:
            call("DELETE", "/expedients/" + expedient, expected=204)
        for identifier in graduate_ids:
            call("DELETE", f"/graduates/{identifier}", expected=204)
        for path, identifier in reversed(fixtures):
            call("DELETE", f"{path}/{identifier}", expected=204)


if __name__ == "__main__":
    verify(sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/api/v1")
