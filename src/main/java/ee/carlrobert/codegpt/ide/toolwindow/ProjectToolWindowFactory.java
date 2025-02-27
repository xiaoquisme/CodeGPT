package ee.carlrobert.codegpt.ide.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.ui.jcef.JBCefBrowser;
import ee.carlrobert.codegpt.client.ClientFactory;
import ee.carlrobert.codegpt.ide.account.AccountDetailsState;
import ee.carlrobert.codegpt.ide.conversations.ConversationsState;
import ee.carlrobert.codegpt.ide.toolwindow.chat.ChatGptToolWindow;
import ee.carlrobert.codegpt.ide.toolwindow.conversations.ConversationsToolWindow;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

public class ProjectToolWindowFactory implements ToolWindowFactory, DumbAware {

  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    var chatToolWindow = new ChatGptToolWindow(project);
    var conversationsToolWindow = new ConversationsToolWindow(project);
    var toolWindowService = project.getService(ToolWindowService.class);
    toolWindowService.setChatToolWindow(chatToolWindow);

    var contentManagerService = project.getService(ContentManagerService.class);
    addContent(toolWindow, chatToolWindow.getContent(), "Chat");
    addContent(toolWindow, conversationsToolWindow.getContent(), "Conversation History");
    addContent(toolWindow, new JBCefBrowser("https://chat.openai.com/chat").getComponent(), "Browser");
    toolWindow.addContentManagerListener(new ContentManagerListener() {
      public void selectionChanged(@NotNull ContentManagerEvent event) {
        var content = event.getContent();
        if ("Conversation History".equals(content.getTabName()) && content.isSelected()) {
          conversationsToolWindow.refresh();
        } else if ("Chat".equals(content.getTabName()) && content.isSelected()) {
          new ClientFactory()
              .getClient()
              .getCreditUsageAsync(creditUsage -> {
                var accountDetails = AccountDetailsState.getInstance();
                accountDetails.totalAmountGranted = creditUsage.getTotalGranted();
                accountDetails.totalAmountUsed = creditUsage.getTotalUsed();
              });
        }
      }
    });

    if (contentManagerService.isChatTabSelected(toolWindow.getContentManager())) {
      var conversation = ConversationsState.getCurrentConversation();
      if (conversation == null) {
        chatToolWindow.displayLandingView();
      } else {
        chatToolWindow.displayConversation(conversation);
      }
    }
  }

  public void addContent(ToolWindow toolWindow, JComponent panel, String displayName) {
    var contentManager = toolWindow.getContentManager();
    var content = contentManager.getFactory().createContent(panel, displayName, false);
    contentManager.addContent(content);
  }
}
