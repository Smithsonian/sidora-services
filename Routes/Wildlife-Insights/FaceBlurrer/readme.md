# Install OpenCV

```
sudo yum install opencv-python
```

# Test Face Blur Script

```
cat testFaceBlur.jpg | python FaceBlurrer.py 99 ./haarcascades/haarcascade_frontalface_alt.xml > output.jpg
```