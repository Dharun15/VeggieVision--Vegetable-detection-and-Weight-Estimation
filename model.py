import torch
import cv2
from ultralytics import YOLO

# Load the model
model_path = r"D:\Projects\last\best (1).pt"
model = YOLO(model_path)

# Perform prediction
def predict(image_path):
    results = model.predict(source=image_path)
    return results
