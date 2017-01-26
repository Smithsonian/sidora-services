
# addProject() TESTS
```bash

curl -v -H "Content-Type:application/xml" \
-d "mciProjectURL=file:///home/jbirkhimer/IdeaProjects/sidora-services/Sidora-MCI/src/test/resources/sample-data/42_0.1.xml" \
-X POST "http://localhost:8282/sidora/rest/mci/test:12345/addProject?option=testOption"

```

# addProjectMultipart() TESTS
```bash

curl -v -H "Content-Type:multipart/form-data" \
-F "mciProject=file:///home/jbirkhimer/IdeaProjects/sidora-services/Sidora-MCI/src/test/resources/sample-data/42_0.1.xml" \
-X POST "http://localhost:8282/sidora/rest/mci/test:12345/addProjectMultipart?option=testOption"

```