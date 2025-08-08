# Face Scanner Server

> 🔍 Facial Recognition Backend using OpenBR (http://openbiometrics.org/)  
> Java-based server for biometric facial identification and matching.

---

## 🧭 Overview

**FaceScanner Server** is a demo backend service built in Java, using the OpenBR biometric framework for facial recognition and identity verification.

It provides HTTP-based endpoints for:

- Face enrollment
- Biometric feature extraction
- Identity matching
- Image validation and logging

This project is ideal for testing or integrating facial recognition in local or enterprise environments.

---

## 🧠 Powered by OpenBR

This project integrates [OpenBR](https://github.com/biometrics/openbr), a powerful open-source framework for biometric recognition, including:

- OpenCV support
- Facial feature landmarks
- Template extraction
- Distance-based identity scoring

---

## ⚙️ Requirements

- Java 17+
- Maven 3.8+
- OpenBR built and installed
- OpenCV (with contrib modules)
- Linux or Windows (x86_64)
- IDE: IntelliJ, Eclipse, or CLI tools

---

## 🔧 Setup

1. **Clone the repository**

```bash
git clone https://github.com/your-org/facescanner-server.git
cd facescanner-server
```

2. **Build the project**

```bash
mvn clean install
```

3. **Configure OpenBR path**

Ensure `openbr` libraries are installed and accessible via system path or `LD_LIBRARY_PATH`.

4. **Run the server**

```bash
java -jar target/facescanner-server.jar
```

---

## 📡 API Endpoints (Example)

| Method | Endpoint        | Description                     |
|--------|------------------|---------------------------------|
| POST   | `/enroll`        | Uploads and registers a face   |
| POST   | `/match`         | Compares a face to known faces |
| GET    | `/status`        | Server health check            |

All responses are in JSON format.

---

## 🧪 Testing

Use tools like Postman or Insomnia to test the API endpoints with base64-encoded images or multipart/form-data.

---

## 🔐 Security

- Logging disabled by default
- Image data is processed in memory
- No biometric data is stored unless explicitly configured

---

## 🧾 License

This project is for demonstration and internal evaluation only.  
All rights reserved © Gaming Innovators. Do not distribute without permission.

---

## 🤝 Credits

- [OpenBR](https://github.com/biometrics/openbr)
- [OpenCV](https://opencv.org/)
- Java Contributors
