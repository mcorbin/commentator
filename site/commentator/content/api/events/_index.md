---
title: Events
weight: 1
disableToc: false
---

**Admin API**

## List events

- **GET** `/api/admin/event/<website>`

---

```
curl -H "Authorization: my-super-token" http://localhost:8787/api/admin/event/mcorbin-fr

[
  {
    "timestamp": 1628364816474,
    "id": "01970a27-eb5f-4ee5-97af-a5a11a427824",
    "article": "foo",
    "message": "New comment 67f99f31-9724-4288-94d7-4fc860dab744 on article foo",
    "comment-id": "67f99f31-9724-4288-94d7-4fc860dab744",
    "type": "new-comment"
  }
]
```

## Delete an event

- **DELETE** `/api/admin/event/<website>`

---

```
curl -H "Authorization: my-super-token" http://localhost:8787/api/admin/event/mcorbin-fr

[
  {
    "timestamp": 1628364816474,
    "id": "01970a27-eb5f-4ee5-97af-a5a11a427824",
    "article": "foo",
    "message": "New comment 67f99f31-9724-4288-94d7-4fc860dab744 on article foo",
    "comment-id": "67f99f31-9724-4288-94d7-4fc860dab744",
    "type": "new-comment"
  }
]
```
