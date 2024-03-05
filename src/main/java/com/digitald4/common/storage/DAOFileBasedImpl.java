package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import org.json.JSONObject;

public class DAOFileBasedImpl extends DAOInMemoryImpl {
  private final String fileName;
  private int writesSinceLastSave = 0;

  public DAOFileBasedImpl(String fileName) {
    this.fileName = fileName;
  }

  public DAOFileBasedImpl loadFromFile() {
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = br.readLine()) != null && !line.isEmpty()) {
        JSONObject entry = new JSONObject(line);
        items.put(entry.getString("idString"), entry.getJSONObject("entity"));
      }
    } catch (FileNotFoundException fnfe) {
      System.out.println("Load file not found, continuing");
    } catch (IOException ioe) {
      throw new DD4StorageException("Error reading load file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
    }

    return this;
  }

  public void saveToFile() {
    if (writesSinceLastSave == 0) {
      return;
    }

    try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
      items.entrySet().stream()
          .sorted(Entry.comparingByKey())
          .map(e -> new JSONObject().put("idString", e.getKey()).put("entity", e.getValue()))
          .forEach(json -> {
            try {
              bw.write(json + "\n");
            } catch (IOException ioe) {
              throw new DD4StorageException("Error writing file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
            }
          });
    } catch (IOException ioe) {
      throw new DD4StorageException("Error writing file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void write(String id, JSONObject json) {
    super.write(id, json);
    if (++writesSinceLastSave >= 100) {
      saveToFile();
      writesSinceLastSave = 0;
    }
  }
}
