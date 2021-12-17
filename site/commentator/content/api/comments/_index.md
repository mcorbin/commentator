---
title: Comments
weight: 1
disableToc: false
---

# Public API

## Add a comment

- **POST** `/api/v1/comment/<website>/<article>`


| Field | Type | Description |
| ------ | ----------- | ----------- |
| author    | string | The author's name |
| author-website | string | The author's website (optional) |
| content    | string | The comment content |
| signature | string | The challenge signature |
| timestamp | string | The challenge timestamp |
| answer    |  string | The challenge answer |

---

```
curl -X POST --header "Content-Type: application/json" --data \ '{"author":"mcorbin", "author-website": "mcorbin.fr", "content":"My comment","challenge":"c1","answer":"5", "signature": "<sign>", "timestamp": 1639688441946}' http://localhost:8787/api/v1/comment/mcorbin-fr/foo

{"message":"Comment added"}
```

## List comments for an article

- **GET** `/api/v1/comment/<website>/<article>`

---

```
curl  http://localhost:8787/api/v1/comment/mcorbin-fr/foo

[
  {
    "content": "My comment",
    "author": "mcorbin",
    "author-website": "mcorbin.fr",
    "id": "0bfe788b-3a06-47fe-8a75-4a3ec6a3b8d9",
    "approved": true,
    "timestamp": 1629101319712
  }
]

```

## Get a random challenge

- **GET** `/api/v1/challenge/<website/<article>`

---

```
curl  http://localhost:8787/api/v1/challenge/mcorbin-fr/foo
{"timestamp": 1629101319712,"question":"1 + 9 = ?", "signature": "<signature>"}
```

# Admin API

## Get a comment

- **GET** `/api/admin/comment/<website>/<article>/<comment-id>`

---

```
curl -H "Authorization: my-super-token" http://localhost:8787/api/admin/comment/mcorbin-fr/foo/0bfe788b-3a06-47fe-8a75-4a3ec6a3b8d9

{
  "content": "My comment",
  "author": "mcorbin",
  "author-website": "mcorbin.fr",
  "id": "0bfe788b-3a06-47fe-8a75-4a3ec6a3b8d9",
  "approved": false,
  "timestamp": 1629101319712
}
```

## List comments for an article

- **GET** `/api/admin/comment/<website>/<article>`

---

```
curl -H "Authorization: my-super-token" http://localhost:8787/api/admin/comment/mcorbin-fr/foo
[
  {
    "content": "My comment",
    "author": "mcorbin",
    "author-website": "mcorbin.fr",
    "id": "0bfe788b-3a06-47fe-8a75-4a3ec6a3b8d9",
    "approved": false,
    "timestamp": 1629101319712
  }
]
```

## Approve a comment

- **POST** `/api/admin/comment/<website>/<article>/<comment-id>`

---

```
curl -X POST -H "Authorization: my-super-token" http://localhost:8787/api/admin/comment/mcorbin-fr/foo/0bfe788b-3a06-47fe-8a75-4a3ec6a3b8d9

{
  "message": "Comment approved"
}
```

## Delete a comment

- **DELETE** `/api/admin/comment/<website>/<article>/<comment-id>`

---

```
curl -X DELETE -H "Authorization: my-super-token" http://localhost:8787/api/admin/comment/mcorbin-fr/foo/0bfe788b-3a06-47fe-8a75-4a3ec6a3b8d9

{
  "message":"Comment deleted"
}
```

## Delete all comments for an article

- **DELETE** `/api/admin/comment/<website>/<article>`

---

```
curl -X DELETE -H "Authorization: my-super-token" http://localhost:8787/api/admin/comment/mcorbin-fr/foo

{
  "message":"Comments deleted"
}
```
