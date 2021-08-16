set -e
tag=$1
lein test

git add .
git commit -m "release ${tag}"
git tag -a "${tag}" -m "release ${tag}"
docker build -t mcorbin/commentator:${tag} .
docker push mcorbin/commentator:${tag}
git push --tags
git push
