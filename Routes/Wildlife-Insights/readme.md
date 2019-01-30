# Install OpenCV

```
sudo yum install opencv-python
```

# Test Face Blur Script

```
cat testFaceBlur.jpg | python FaceBlurrer.py 99 ./haarcascades/haarcascade_frontalface_alt.xml > output.jpg
```

# Run UpdateFGDC.py

> **NOTE: running with debug param will save updated FGDC to local file instead of updating the fedora FGDC datastream**

```
python updateFGDC.sh <fedora-user> <fedora-password> <host> <debug-logging>

ex. python updateFGDC.sh someUN somePW some-host.com debug
```