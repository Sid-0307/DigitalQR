package com.example.dqr;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GenerateQRActivity extends AppCompatActivity {

    private EditText tagInput,filePathText;
    private TextView qr_text,generatingText;
    private Button generateButton;
    private ImageView qrCodeImage,fileInputButton;

    private ProgressBar progressBar;

    private Uri fileUri;
    private String fileType;

    ActivityResultLauncher<Intent> resultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        qr_text = findViewById(R.id.qr_text);
        fileInputButton = findViewById(R.id.fileInput);
        tagInput = findViewById(R.id.tagInput);
        generateButton = findViewById(R.id.generateQRButton);
        qrCodeImage = findViewById(R.id.generatedQRCode);
        generatingText= findViewById(R.id.generatingText);
        progressBar = findViewById(R.id.progressBar);
        filePathText = findViewById(R.id.filePathText);

        fileInputButton.setOnClickListener(v -> openFileChooser());

        generateButton.setOnClickListener(v -> {
            String tag = tagInput.getText().toString().trim();

            if (fileUri == null) {
                Toast.makeText(GenerateQRActivity.this, "Please select a file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (tag.isEmpty()) {
                Toast.makeText(GenerateQRActivity.this, "Please enter tag", Toast.LENGTH_SHORT).show();
                return;
            }

            String googleMapsUrl = "https://www.google.com/maps/search/" + Uri.encode(tag);
            qr_text.setVisibility(View.GONE);
            qrCodeImage.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            generatingText.setVisibility(View.VISIBLE);
            uploadFileAndGenerateQR(googleMapsUrl, fileUri);
        });

        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result != null && result.getData() != null) {
                    fileUri = result.getData().getData();

                    String mimeType = getContentResolver().getType(fileUri);
                    if (mimeType != null) {
                        if (mimeType.startsWith("image/")) {
                            fileType = "image";
                        } else if (mimeType.equals("application/pdf")) {
                            fileType = "pdf";
                        } else if (mimeType.startsWith("text/")) {
                            fileType = "text";
                        }
                        String filePath = fileUri.toString();
                        filePathText.setText(filePath);
                    }
                } else {
                    Toast.makeText(GenerateQRActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {
                "image/*",
                "application/pdf",
                "text/*"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        resultLauncher.launch(intent);
    }


    private void uploadFileAndGenerateQR(String tag, Uri fileUri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String fileName = UUID.randomUUID().toString();

        String mimeType = getContentResolver().getType(fileUri);
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                if (mimeType.equals("image/jpeg")) {
                    fileName += ".jpg";
                } else if (mimeType.equals("image/png")) {
                    fileName += ".png";
                } else if (mimeType.equals("image/heic")) {
                    fileName += ".heic";
                } else {
                    fileName += ".jpg";
                }
            } else if (mimeType.equals("application/pdf")) {
                fileName += ".pdf";
            } else if (mimeType.startsWith("text/")) {
                fileName += ".txt";
            }
        }

        StorageReference fileRef = storageRef.child("files/" + fileName);

        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);

            if (inputStream != null) {
                UploadTask uploadTask = fileRef.putStream(inputStream);
                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String fileUrl = uri.toString();
                        generateAndUploadHtml(tag, fileUrl, fileType);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(GenerateQRActivity.this, "Failed to get file URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(GenerateQRActivity.this, "File upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(GenerateQRActivity.this, "Unable to open file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(GenerateQRActivity.this, "Error resolving file URI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void generateQRCode(String fileUrl) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(fileUrl, BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImage.setImageBitmap(bitmap);
            progressBar.setVisibility(View.GONE);
            generatingText.setVisibility(View.GONE);
            qrCodeImage.setVisibility(View.VISIBLE);
            Toast.makeText(GenerateQRActivity.this, "QR generated successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(GenerateQRActivity.this, "Error generating QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateAndUploadHtml(String tagUrl, String fileUrl, String fileType) {
        String htmlContent = generateHtmlContent(tagUrl, fileUrl, fileType);

        try {
            File htmlFile = new File(getCacheDir(), "file_preview.html");
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            outputStream.write(htmlContent.getBytes());
            outputStream.close();

            Uri htmlUri = Uri.fromFile(htmlFile);
            StorageReference htmlRef = FirebaseStorage.getInstance().getReference().child("html/" + UUID.randomUUID().toString() + ".html");

            UploadTask uploadTask = htmlRef.putFile(htmlUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                htmlRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String htmlFileUrl = uri.toString();
                    generateQRCode(htmlFileUrl);
                }).addOnFailureListener(e -> {
                    Toast.makeText(GenerateQRActivity.this, "Failed to get HTML file URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(GenerateQRActivity.this, "Failed to upload HTML file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Toast.makeText(GenerateQRActivity.this, "Error generating HTML file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String generateHtmlContent(String tagUrl, String fileUrl, String fileType) {
        StringBuilder htmlContent = new StringBuilder();
        StringBuilder textContent = new StringBuilder();
        if(fileType.equals("text")) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    textContent.append(line).append("\n");
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        htmlContent.append("<html><body style='text-align: center; font-family: Arial;'>");

        switch (fileType) {
            case "image":
                htmlContent.append("<img alt='HEIC image not supported by browser' src='").append(fileUrl)
                        .append("' style='max-width: 90%; height: auto;' /><br>");
                break;

            case "pdf":
                htmlContent.append("<embed src='").append(fileUrl)
                        .append("' type='application/pdf' width='100%' height='600px' /><br>");
                break;

            case "text":
                htmlContent.append("<pre style='white-space: pre-wrap; word-wrap: break-word; font-size: 40px'>")
                    .append(textContent.toString())
                    .append("</pre>");
                break;
        }

        htmlContent.append("<p style='font-size: 40px;'><a href='")
                .append(tagUrl)
                .append("' target='_blank' style='color: #EE5857; text-decoration: underline;'>")
                .append("Click here to view on Maps</a></p>")
                .append("<button onclick='downloadFile()' style='font-size: 20px; padding: 10px 20px; background-color: #EE5857; color: white; border: none; cursor: pointer;'>")
                .append("Download File")
                .append("</button>")
                .append("<script>")
                .append("  function downloadFile() {")
                .append("      var a = document.createElement('a');")
                .append("      a.href = '").append(fileUrl).append("';")
                .append("      a.download = 'downloaded_file.").append(fileType).append("';")
                .append("      a.click();")
                .append("  }")
                .append("</script>")
                .append("</body></html>");

        return htmlContent.toString();
    }
}