
# addProject() TESTS
```bash

curl -v -H "Content-Type:application/xml" \
-d "@/home/jbirkhimer/IdeaProjects/sidora-services/Sidora-MCI/src/test/resources/sample-data/42_0.1.xml" \
-X POST "http://localhost:8181/sidora/rest/mci/test:12345/addProject?option=testOption"

```