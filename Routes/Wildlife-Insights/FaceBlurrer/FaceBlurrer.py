import sys
import cv2
import numpy as np


def faceBlur(img, args):
    blur_value = int(args[1])
    classifier = args[2]

    # Preprocess the image
    # Converts an image from one color space to another.
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gray = cv2.equalizeHist(gray)

    # Detect faces using classifier and
    faceCascade = cv2.CascadeClassifier(classifier)
    faces = faceCascade.detectMultiScale(gray, 1.1, 2, 0 | cv2.CASCADE_SCALE_IMAGE, (20, 20))

    if len(faces) != 0:  # If there are faces in the images
        for (x, y, w, h) in faces:
            # apply a blur on this new face image
            img[y:y + h, x:x + w] = blur(img[y:y + h, x:x + w], (blur_value, blur_value))
    # else:
    #     print("NO FACE'S FOUND")
    # python3 needs stdout.buffer to read binary data from stdin, python2 does not
    if python_version == 3:
        sys.stdout.buffer.write(cv2.imencode('.jpg', img)[1])
    elif python_version == 2:
        sys.stdout.write(cv2.imencode('.jpg', img)[1])
    # else:
        print("must use python 2 or greater")


def blur(img, blur_value):
    # blur_img = cv2.dilate(img, np.ones((21,21),'uint8'))
    # blur_img = cv2.blur(img, (21,21), 50)
    # blur_img = cv2.GaussianBlur(img, (99, 99), 0)
    blur_img = cv2.GaussianBlur(img, blur_value, 30)
    return blur_img


python_version = sys.version_info.major
# python3 needs stdin.buffer to read binary data from stdin, python2 does not
if python_version == 3:
    stdin = sys.stdin.buffer.read()
elif python_version == 2:
    stdin = sys.stdin.read()
# else:
#     print("must use python 2 or greater")
nparr = np.frombuffer(stdin, np.uint8)
img_np = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
faceBlur(img_np, sys.argv)