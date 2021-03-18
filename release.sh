set -e
tag=$1
lein test
lein uberjar
git tag -a "${tag}" -m "release ${tag}"
docker build -t mcorbin/commentator:${tag} .
docker push mcorbin/commentator:${tag}
git push --tags
