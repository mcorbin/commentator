# Commentator

## Public API

### Add comment

curl -X POST --header "Content-Type: application/json" --data \ '{"author":"mcorbin","content":"My comment","challenge":"op1","answer":"10"}' \
http://localhost:8787/api/v1/comment/foo/

### List comments for an article

curl http://localhost:8787/api/v1/comment/foo

## Admin API

### List events

curl -H "Authorization: OIOzkfiZzrzrIIejj" \
http://localhost:8787/api/admin/event

### List all comments for an article

curl -H "Authorization: OIOzkfiZzrzrIIejj" http://localhost:8787/api/admin/comment/foo

### Approve a comment

curl -X POST -H "Authorization: OIOzkfiZzrzrIIejj" \ http://localhost:8787/api/admin/comment/foo/565f9cfa-0b22-4870-9e89-67c1dbd710ec

### Get a specific comment for an article

curl -H "Authorization: OIOzkfiZzrzrIIejj" \
http://localhost:8787/api/admin/comment/foo/565f9cfa-0b22-4870-9e89-67c1dbd710ec

### Delete a specific comment

curl -X DELETE -H "Authorization: OIOzkfiZzrzrIIejj" \
http://localhost:8787/api/admin/comment/foo/565f9cfa-0b22-4870-9e89-67c1dbd710ec



