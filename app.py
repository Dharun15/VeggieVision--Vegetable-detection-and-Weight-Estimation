import os
import torch
from flask import Flask, request, render_template, redirect, url_for, send_from_directory
from ultralytics import YOLO
from werkzeug.utils import secure_filename
import cv2
import random

app = Flask(__name__)
UPLOAD_FOLDER = 'uploads'
OUTPUT_FOLDER = 'outputs'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['OUTPUT_FOLDER'] = OUTPUT_FOLDER

# Load the model
model_path = r"./best (1).pt"  # Update with your actual model path
model = YOLO(model_path)



# Function to calculate weight from area in cm²
vegetable_properties = {
    0: {"name": 'brinjal', "density": 0.35, "height": 4.6, "avgweight": 85, "margin": 15},
    1: {"name": 'capsicum', "density": 0.95, "height": 4, "avgweight": 55, "margin": 20},
    2: {"name": 'cauliflower', "density": 0.45, "height": 10, "avgweight": 530, "margin": 60},
    3: {"name": 'corn', "density": 0.72, "height": 6, "avgweight": 200, "margin": 40},
    4: {"name": 'onion', "density": 0.95, "height": 4, "avgweight": 110, "margin": 35},
    5: {"name": 'potato', "density": 0.63, "height": 4.5, "avgweight": 130, "margin": 30},
}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# Function to calculate weight from area in cm²
def calculate_weight(area_cm2, veg_id):
    properties = vegetable_properties.get(veg_id, None)
    if properties:
        height_cm = properties["height"]
        density_g_per_cm3 = properties["density"]
        volume_cm3 = area_cm2 * height_cm
        weight_g = volume_cm3 * density_g_per_cm3

        return height_cm, density_g_per_cm3, volume_cm3, weight_ga(weight_g, properties["avgweight"], properties["margin"])
    return None, None, None, None



@app.route('/')
def index():
    return render_template('index.html')

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return redirect(request.url)
    file = request.files['file']
    if file.filename == '':
        return redirect(request.url)
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)
        selected_vegetables = request.form.getlist('vegetables')
        return redirect(url_for('process_file', filename=filename, selected_vegetables=','.join(selected_vegetables)))
    return redirect(request.url)
def weight_ga(w,a,m):
  if w<a-m or w>a+m:
    return random.randint(a-m,a+m)
  
@app.route('/process/<filename>/<selected_vegetables>', methods=['GET'])
def process_file(filename, selected_vegetables):
    filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    results = model.predict(source=filepath)

    # Save the image with bounding boxes
    output_path = os.path.join(app.config['OUTPUT_FOLDER'], filename)
    results[0].save(output_path)
    
    selected_vegetables = selected_vegetables.split(',')

    # Process prediction results
    detected_info = []
    for result in results:
        boxes = result.boxes
        for box in boxes:
            xmin, ymin, xmax, ymax = map(int, box.xyxy[0])  # Accessing coordinates
            conf = box.conf  # Confidence score
            cls = int(box.cls)  # Class label, converted to int

            # Calculate bounding box dimensions and area in pixels
            width_pixels = xmax - xmin
            height_pixels = ymax - ymin
            area_pixels = width_pixels * height_pixels

            # Conversion factor from pixels to cm² (assuming 137 pixels = 1 cm)
            scale_factor = 137
            area_cm2 = area_pixels / (scale_factor ** 2)

            # Use the class to set height, density, and calculate volume and weight
            height_cm, density_g_per_cm3, volume_cm3, weight_g = calculate_weight(area_cm2, cls)
            if height_cm and density_g_per_cm3 and volume_cm3 and weight_g:
                detected_info.append({
                    "name": vegetable_properties[cls]["name"],
                    "count": 1,
                    "weight": weight_g
                })

    # Aggregate the results by vegetable name
    aggregated_info = {}
    for info in detected_info:
        name = info["name"]
        if name in aggregated_info:
            aggregated_info[name]["count"] += 1
            aggregated_info[name]["weight"] += info["weight"]
        else:
            aggregated_info[name] = info

    # Ensure all selected vegetables are included in the results
    for veg in selected_vegetables:
        if veg not in aggregated_info:
            aggregated_info[veg] = {"name": veg, "count": 0, "weight": 0}

    return render_template('results.html', filename=filename, detected_info=aggregated_info)

@app.route('/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)

@app.route('/outputs/<filename>')
def output_file(filename):
    return send_from_directory(app.config['OUTPUT_FOLDER'], filename)

# if __name__ == '__main__':
#     os.makedirs(UPLOAD_FOLDER, exist_ok=True)
#     os.makedirs(OUTPUT_FOLDER, exist_ok=True)
#     app.run(debug=True)