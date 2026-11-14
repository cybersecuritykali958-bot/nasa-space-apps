def test_health(client):
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert body["database"] == "ok"


def test_list_datasets(client):
    assert client.get("/api/datasets").json() == ["sample"]


def test_get_dataset(client):
    assert client.get("/api/datasets/sample").json() == {"ok": True}


def test_get_dataset_missing(client):
    assert client.get("/api/datasets/nope").status_code == 404


def test_get_dataset_rejects_traversal(client):
    assert client.get("/api/datasets/..%2F..%2Fsecret").status_code == 404
