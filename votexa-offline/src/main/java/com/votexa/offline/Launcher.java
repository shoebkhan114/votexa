package com.votexa.offline;

import com.votexa.offline.ui.MainApp;
import javafx.application.Application;

/** Plain main class so the shaded (fat) jar can start JavaFX without a module path. */
public final class Launcher {
    public static void main(String[] args) { Application.launch(MainApp.class, args); }
}
