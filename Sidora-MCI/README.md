
# addProject() TESTS
```bash

curl -v -H "Content-Type:application/xml" \
-d "@/home/jbirkhimer/IdeaProjects/sidora-services/Sidora-MCI/src/test/resources/sample-data/42_0.1.xml" \
-X POST "http://localhost:8181/cxf/sidora/rest/mci/test:12345/addProject?option=testOption"

```

The approach to a failed user match is to use a default person.
It will be 'Ann N’Gadi'

