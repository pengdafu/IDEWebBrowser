package com.pengdafu.idewebbrowser;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class Window implements ToolWindowFactory, DumbAware {
    private static final String LAST_URL_KEY = "com.pengdafu.idewebbrowser.lastUrl";

    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField textField = new JTextField();
        JBCefBrowser browser = getBrowser();

        // Create buttons
        JButton backButton = new JButton(AllIcons.Actions.Back);
        JButton forwardButton = new JButton(AllIcons.Actions.Forward);
        JButton refreshButton = new JButton(AllIcons.Actions.Refresh);

        // Add action listeners
        backButton.addActionListener(e -> {
            if (browser.getCefBrowser().canGoBack()) {
                browser.getCefBrowser().goBack();
            }
        });

        forwardButton.addActionListener(e -> {
            if (browser.getCefBrowser().canGoForward()) {
                browser.getCefBrowser().goForward();
            }
        });

        refreshButton.addActionListener(e -> browser.getCefBrowser().reload());

        // Add buttons to a panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(refreshButton);

        String lastUrl = PropertiesComponent.getInstance().getValue(LAST_URL_KEY);
        if (lastUrl != null) {
            browser.loadURL(lastUrl);
            textField.setText(lastUrl);
        }

        textField.addActionListener(e -> {
            // handle the event when enter key is pressed
            String url = textField.getText();
            browser.loadURL(url);
        });

        // Add textField and buttonPanel to a single panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(textField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(browser.getComponent(), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private JBCefBrowser getBrowser() {
        JBCefBrowser browser = JBCefBrowser.createBuilder().setCreateImmediately(true).setClient(JBCefApp.getInstance().createClient()).setOffScreenRendering(false).setEnableOpenDevToolsMenuItem(true).build();
        browser.setErrorPage((errorCode, errorText, failedUrl) -> (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED) ? null : JBCefBrowserBase.ErrorPage.DEFAULT.create(errorCode, errorText, failedUrl));
        browser.setProperty(JBCefBrowser.Properties.FOCUS_ON_SHOW, Boolean.TRUE);
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser cefBrowser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                if (!isLoading) {
                    String url = cefBrowser.getURL();
                    PropertiesComponent.getInstance().setValue(LAST_URL_KEY, url);
                }
            }
        }, browser.getCefBrowser());
        browser.createImmediately();
        return browser;
    }
}
