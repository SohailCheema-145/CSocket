package com.cheema.csocket;

import java.io.PrintWriter;

public interface CInterface {
    void onDataSent(String data);

    void onDataReceived(String data, PrintWriter printWriter);

    void onFailure(String data, String error);
}
