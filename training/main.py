import numpy as np
import struct
import tensorflow as tf
from tensorflow.keras import layers
from tensorflow.keras.models import Sequential
from tensorflow import lite
import wandb
from wandb.keras import WandbCallback
from sklearn.model_selection import train_test_split

# Setup wandb logging

wandb.login()

wandb.init(name='Training run', 
           project='Letter Classification Project (Embedded and Distributed AI)')

# Get images from the dataset

with open('emnist-letters-train-images-idx3-ubyte', 'rb') as f:
    magic, size = struct.unpack('>II', f.read(8))
    nrows, ncols = struct.unpack('>II', f.read(8))
    data = np.fromfile(f, dtype=np.dtype(np.uint8)).newbyteorder(">")
    data = data.reshape((size, nrows, ncols))

# Get class labels from the dataset

with open('emnist-letters-train-labels-idx1-ubyte', 'rb') as i:
    magic, size = struct.unpack('>II', i.read(8))
    data_1 = np.fromfile(i, dtype=np.dtype(np.uint8)).newbyteorder(">")
    
x, y = data, data_1

# Substract 1 from all the class labels since we need the classes to be from 0 to 25 and not 1 to 26

for i in range(len(y)):
    y[i] -= 1

# Stratified sampling of 80/10/10

test_ratio = 1 / 10
validation_ratio = 1 / 10 * (1 - test_ratio)

x_remaining, x_test, y_remaining, y_test = train_test_split(x, y, test_size=test_ratio, stratify=y)
print(x_remaining.shape)
print(y_remaining.shape)
x_train, x_val, y_train, y_val = train_test_split(x_remaining, y_remaining, test_size=validation_ratio, stratify=y_remaining)

num_classes = 26

# Setup model

model = Sequential([
    layers.experimental.preprocessing.Rescaling(1./255, input_shape=(28, 28, 1)),
    layers.Conv2D(15, 25, padding='same', activation='relu'),
    layers.MaxPooling2D((15, 15), padding='same'),
    layers.Conv2D(30, 10, padding='same', activation='relu'),
    layers.MaxPooling2D((5, 5), padding='same'),
    layers.Conv2D(200, 1, padding='same', activation='relu'),
    layers.Flatten(),
    layers.Dense(150, activation='relu'),
    layers.Dense(num_classes)
])

model.compile(optimizer='adam',
              loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
              metrics=['accuracy'])

# Train model and log do wandb

epochs = 20

history = model.fit(x=x_train,
    y=y_train,
    validation_data=(x_val, y_val),
    epochs=epochs,
    callbacks=[WandbCallback()]
)

# Get accuracy with testing set

test_results = []

for i in range(len(x_test)):
    if np.argmax(model(x_test[i].reshape(1, 28, 28))) == y_test[i]:
        test_results.append(1)
    else:
        test_results.append(0)

test_accuracy = sum(test_results) / len(test_results)

print("Tested accuracy: " + str(test_accuracy))

# Convert to TensorFlow Lite model and save

converter = lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open('letter_classification_model.tflite', 'wb') as f:
  f.write(tflite_model)