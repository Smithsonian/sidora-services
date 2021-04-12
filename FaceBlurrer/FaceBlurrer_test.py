import os
import sys
import cv2
import face_recognition
import numpy as np


def main():
    for f in os.listdir(sys.argv[1] + "/output"):
        # print("remove = " + f)
        os.remove(sys.argv[1] + "/output/" + f)

    for file in os.listdir(sys.argv[1]):
        # inputFile = sys.argv[1]
        path = os.path.join(sys.argv[1], file)
        if os.path.isfile(path):
            inputFile = sys.argv[1] + "/" + file
            # blurFactor = float(sys.argv[2])
            outputFile = os.path.split(os.path.abspath(inputFile))[0] + "/output/" + os.path.splitext(os.path.basename(inputFile))[0] + "_blurred.jpg"
            print(inputFile)
            img = cv2.imread(inputFile)

            # Preprocess the image
            # Converts an image from one color space to another.
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            gray = cv2.equalizeHist(gray)

            # faceCascade = cv2.CascadeClassifier('haarcascades/haarcascade_frontalface_default.xml')
            # faceCascade = cv2.CascadeClassifier('haarcascades/haarcascade_frontalface_alt.xml')
            # faceCascade = cv2.CascadeClassifier('haarcascades/haarcascade_frontalface_alt2.xml')
            # faceCascade = cv2.CascadeClassifier('haarcascades/haarcascade_frontalface_alt_tree.xml')
            # faceCascade = cv2.CascadeClassifier('haarcascades/haarcascade_profileface.xml')
            # faceCascade = cv2.CascadeClassifier('haarcascades/haarcascade_eye_tree_eyeglasses.xml')
            # faceCascade = cv2.CascadeClassifier("haarcascades/lbpcascade_frontalface_improved.xml")

            # faces = faceCascade.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=5, minSize=(20, 20))  # original
            # faces = faceCascade.detectMultiScale(gray, 1.3, 4, 0 | cv2.CASCADE_SCALE_IMAGE, (20, 20))
            # faces = faceCascade.detectMultiScale(gray, 1.1, 2, 0 | cv2.CASCADE_SCALE_IMAGE, (20, 20))
            # faces = faceCascade.detectMultiScale(gray, 1.5)

            # print("Found {} face(s).".format(len(faces)))

            ############################################################################################################

            # Resize frame of video to 1/4 size for faster face detection processing
            small_frame = cv2.resize(img, (0, 0), fx=0.25, fy=0.25)

            # Find all the faces and face encodings in the current frame of video
            face_locations = face_recognition.face_locations(small_frame, model="cnn")
            # face_locations = face_recognition.face_locations(small_frame, 2, model="cnn") # Higher numbers find smaller faces.
            # face_locations = face_recognition.face_locations(small_frame) #The default model is "hog"

            print("Found {} face(s).".format(len(face_locations)))

            ############################################################################################################

            # face_blur(inputFile, outputFile, img, faces)
            # face_blur2(inputFile, outputFile, img, faces)
            # face_blur3(inputFile, outputFile, img, faces, gray)
            # face_blur4(inputFile, outputFile, img, faces)
            face_blur5(inputFile, outputFile, img, face_locations)
            print("----------------------------------------")

def save_face(inputFile, img, test):
    face_file_name = os.path.split(os.path.abspath(inputFile))[0] + "/output/" + os.path.splitext(os.path.basename(inputFile))[0] + "_face_" + test + ".jpg"
    cv2.imwrite(face_file_name, img)
    print("Saveing face part to: " + face_file_name)

def blur(img):
    # blur_img = cv2.dilate(img, np.ones((21,21),'uint8'))
    # blur_img = cv2.blur(img, (21,21), 50)
    # blur_img = cv2.GaussianBlur(img, (99, 99), 0)
    blur_img = cv2.GaussianBlur(img, (99, 99), 30)
    return blur_img


def preview_img(img):
    cv2.namedWindow('facedetect', cv2.WINDOW_NORMAL)
    cv2.imshow('facedetect', img)

    cv2.waitKey(0)


def face_blur(inputFile, outputFile, img, faces):
    print("Starting face_blur")

    if len(faces) != 0:  # If there are faces in the images
        for (x, y, w, h) in faces:
            # save_face(inputFile, img[y:y + h, x:x + w], str(y) + "_1st")

            cv2.rectangle(img, (x, y), (x + w, y + h), (255, 255, 0), 5)

            # apply a blur on this new face image
            img[y:y + h, x:x + w] = blur(img[y:y + h, x:x + w])

        cv2.imwrite(outputFile, img)
        # preview_img(img)
    else:
        print("NO FACE'S FOUND")


def face_blur2(inputFile, outputFile, img, faces):
    print("Starting face_blur2")

    result_image = img.copy()

    if len(faces) != 0:  # If there are faces in the images
        for f in faces:  # For each face in the image
            # Get the origin co-ordinates and the length and width till where the face extends
            x, y, w, h = f

            # get the rectangle img around all the faces
            # cv2.rectangle(img, (x, y), (x + w, y + h), (255, 255, 0), 5)

            # work only on face part
            sub_face = img[y:y + h, x:x + w]

            # apply a blur on this new face image
            sub_face = blur(sub_face)

            # merge this blurry rectangle to our final image
            result_image[y:y + sub_face.shape[0], x:x + sub_face.shape[1]] = sub_face

            # save_face(inputFile, sub_face, str(y) + "_1st")

            # save result
            cv2.imwrite(outputFile, result_image)
            # preview_img(img)
    else:
        print("NO FACE'S FOUND")


def face_blur3(inputFile, outputFile, img, faces, gray):
    print("Starting face_blur3")


    if len(faces) != 0:  # If there are faces in the images
        result_image = img.copy()
        for (x, y, w, h) in faces:
            # convert selection to grayscale and blur
            roi_gray = gray[y:y + h, x:x + w]

            # apply a blur on this new face image
            roi_gray = blur(roi_gray)

            # convert selection back to BRG
            roi_gray = cv2.cvtColor(roi_gray, cv2.COLOR_GRAY2BGR)

            # merge this blurry rectangle to our final image
            result_image[y:y + roi_gray.shape[0], x:x + roi_gray.shape[1]] = roi_gray

            save_face(inputFile, roi_gray, str(y) + "_1st")

            # save result
            cv2.imwrite(outputFile, result_image)
            preview_img(result_image)
    else:
        print("NO FACE'S FOUND")


def detect(img, cascade, test):
    faces = cascade.detectMultiScale(img, 1.1, 2, 0 | cv2.CASCADE_SCALE_IMAGE, (20, 20))

    print("Found {} face(s).".format(len(faces)))

    if len(faces) == 0:
        return []
    faces[:, 2:] += faces[:, :2]

    # if len(faces) != 0:
    # result_image = img.copy()
    for x, y, w, h in faces:
        sub_face = img[y:h, x:w]
        face_file_name = "./test-images/output/face_" + str(x) + "_" + str(y) + "_" + test + ".jpg"
        # cv2.imwrite(face_file_name, sub_face)
        # preview_img(sub_face)

    return faces


def draw_rects(img, faces, color):
    for x, y, w, h in faces:
        cv2.rectangle(img, (x, y), (w, h), color, 5)
        # img[y:y + h, x:x + w] = cv2.GaussianBlur(img[y:y + h, x:x + w], (99, 99), 0)
        # return img
        # cv2.GaussianBlur(img[y:y + h, x:x + w], (99, 99), 0)


def face_blur4(inputFile, outputFile, img, faces):
    print("Starting face_blur4")

    cascad_body = "haarcascades/haarcascade_upperbody.xml"
    # cascad_body = "haarcascades/haarcascade_fullbody.xml"
    body = cv2.CascadeClassifier(cascad_body)

    cascade_fn = "haarcascades/haarcascade_frontalface_alt.xml"
    cascade = cv2.CascadeClassifier(cascade_fn)

    # nested_fn = "haarcascades/haarcascade_frontalface_alt.xml"
    # nested_fn = "haarcascades/haarcascade_profileface.xml"
    # nested_fn = "haarcascades/haarcascade_frontalface_alt2.xml"
    # nested_fn = "haarcascades/haarcascade_eye.xml"
    # nested_fn = "haarcascades/haarcascade_eye_tree_eyeglasses.xml"
    # nested = cv2.CascadeClassifier(nested_fn)

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gray = cv2.equalizeHist(gray)

    # faces = detect(gray, cascade, "1st")
    faces = detect(gray, body, "1st")
    vis = img.copy()
    draw_rects(vis, faces, (255, 255, 0))
    # preview_img(vis)

    if not cascade.empty():
        for x, y, w, h in faces:
            roi = gray[y:h, x:w]
            # preview_img(roi)
            vis_roi = vis[y:h, x:w]
            # preview_img(vis_roi)
            subrects = detect(roi.copy(), cascade, "2nd")
            draw_rects(vis_roi, subrects, (255, 0, 0))
            # preview_img(vis_roi)

    cv2.imwrite(outputFile, vis)
    # preview_img(vis)


def face_blur5(inputFile, outputFile, img, face_locations):
    print("Starting face_blur5")

    if len(face_locations) != 0:  # If there are faces in the images
        # Display the results
        for top, right, bottom, left in face_locations:
            # Scale back up face locations since the frame we detected in was scaled to 1/4 size
            top *= 4
            right *= 4
            bottom *= 4
            left *= 4

            # cv2.rectangle(img, (top, right), (top + bottom, right + left), (255, 255, 0), 5)
            # preview_img(img)

            # Extract the region of the image that contains the face
            face_image = img[top:bottom, left:right]
            # preview_img(face_image)

            # Blur the face image
            face_image = cv2.GaussianBlur(face_image, (99, 99), 30)
            # preview_img(face_image)

            # Put the blurred face region back into the frame image
            img[top:bottom, left:right] = face_image

        # Display the resulting image
        preview_img(img)
        cv2.imwrite(outputFile, img)
    else:
        print("No Faces Found!!!")



if __name__ == "__main__":
    main()

cv2.destroyAllWindows()
