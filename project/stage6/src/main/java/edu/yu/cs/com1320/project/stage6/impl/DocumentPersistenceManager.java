package edu.yu.cs.com1320.project.stage6.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Type;
import java.net.URI;

import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import edu.yu.cs.com1320.project.stage6.PersistenceManager;
import edu.yu.cs.com1320.project.stage6.Document;

public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File baseDir;

    public DocumentPersistenceManager(File baseDir) {
        if (baseDir != null)
            this.baseDir = baseDir;
        else
            this.baseDir = new File(System.getProperty("user.dir"));
    }

    private class DocumentSerializer implements JsonSerializer<Document> {
        @Override
        public JsonElement serialize(Document doc, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uri", doc.getKey().toString());

            if (doc.getDocumentTxt() != null) {
                jsonObject.addProperty("isBinary", false);
                jsonObject.addProperty("content", doc.getDocumentTxt());
            } else {
                jsonObject.addProperty("isBinary", true);
                jsonObject.addProperty("content", Base64.getEncoder().encodeToString(doc.getDocumentBinaryData()));
            }

            Gson gson = new Gson();

            JsonElement metaDataJson = gson.toJsonTree(doc.getMetadata(), new TypeToken<Map<String, String>>() {
            }.getType());
            jsonObject.add("metaData", metaDataJson);

            JsonElement wordsMapJson = gson.toJsonTree(doc.getWordMap(), new TypeToken<Map<String, Integer>>() {
            }.getType());
            jsonObject.add("wordsMap", wordsMapJson);

            return jsonObject;
        }
    };

    private class DocumentDeserializer implements JsonDeserializer<Document> {
        @Override
        public Document deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            JsonObject jobject = json.getAsJsonObject();

            Gson gson = new Gson();
            Type wordsMapType = new TypeToken<HashMap<String, Integer>>() {
            }.getType();
            Type metaDataMapType = new TypeToken<HashMap<String, String>>() {
            }.getType();

            HashMap<String, Integer> wordsMap = gson.fromJson(jobject.get("wordsMap"), wordsMapType);
            HashMap<String, String> metaDataMap = gson.fromJson(jobject.get("metaData"), metaDataMapType);

            URI uri = URI.create(jobject.get("uri").getAsString());
            String content = jobject.get("content").getAsString();

            Document doc;
            if (jobject.get("isBinary").getAsBoolean() == false) {
                doc = new DocumentImpl(uri, content, wordsMap);
            } else {
                byte[] binaryData = Base64.getDecoder().decode(content);
                doc = new DocumentImpl(uri, binaryData);
            }

            doc.setMetadata(metaDataMap);
            return doc;
        }
    }

    private String getDirectory(URI uri) {
        String dir = "";
        dir += this.baseDir;
        dir += "/" + uri.getAuthority() + uri.getPath() + ".json";
        return dir;
    }

    public void serialize(URI key, Document val) throws IOException {
        if (key == null || val == null) {
            return;
        }
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Document.class, new DocumentSerializer())
                .create();

        String json = gson.toJson(val, Document.class);
        File file = new File(getDirectory((URI) key));
        try {
            File directory = new File(file.getParentFile().getAbsolutePath());
            directory.mkdirs();
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (IOException ignored) {
        }
    }

    public Document deserialize(URI key) throws IOException {
        if (key == null) {
            return null;
        }
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Document.class, new DocumentDeserializer())
                .create();

        File file = new File(getDirectory((URI) key));
        if (!file.exists()) {
            throw new IOException();
        }
        FileReader reader = new FileReader(file);
        Document doc = gson.fromJson(reader, Document.class);
        reader.close();
        this.delete(key);
        deleteEmptyFolders(file.getParentFile());
        return (Document) doc;
    }

    private void deleteEmptyFolders(File folder) {
        if (folder == null || !folder.isDirectory()) {
            return;
        }

        File[] files = folder.listFiles();
        if (files != null && files.length == 0) {
            folder.delete();
            // Recursively delete empty parent folders
            deleteEmptyFolders(folder.getParentFile());
        }
    }

    /**
     * delete the file stored on disk that corresponds to the given key
     * @param key
     * @return true or false to indicate if deletion occured or not
     * @throws IOException
     */
    public boolean delete(URI key) throws IOException {
        if (key == null) {
            return false;
        }
        File file = new File(getDirectory((URI) key));
        boolean deleted = file.delete();
        deleteEmptyFolders(file.getParentFile());
        return deleted;
    }
}
