package com.digitalsanctuary.spring.demo.user.ui.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;

/**
 * Page object for delete account page
 */
public class DeleteAccountPage {

    private final SelenideElement DELETE_ACCOUNT_BUTTON = Selenide.$x("//button[@type='submit']");
    private final SelenideElement DELETE_CONFIRMATION_MODAL = Selenide.$x("//div[@id='deleteConfirmationModal']");
    private final SelenideElement CONFIRMATION_INPUT = Selenide.$x("//input[@id='deleteConfirmationInput']");
    private final SelenideElement CONFIRM_DELETION_BUTTON = Selenide.$x("//button[@id='confirmDeletionButton']");
    private final SelenideElement CANCEL_BUTTON = Selenide.$x("//button[contains(@class, 'btn-secondary')]");
    private final SelenideElement GLOBAL_MESSAGE = Selenide.$x("//div[@id='globalMessage']");
    private final SelenideElement GLOBAL_ERROR = Selenide.$x("//div[@id='globalError']");

    public DeleteAccountPage(String url) {
        Selenide.open(url);
    }

    public DeleteAccountPage() {
        // For navigation to this page from other pages
    }

    /**
     * Start the account deletion process by clicking the delete button
     */
    public DeleteAccountPage startDeletion() {
        DELETE_ACCOUNT_BUTTON.click();
        // Wait for modal to appear
        DELETE_CONFIRMATION_MODAL.should(appear);
        return this;
    }

    /**
     * Complete account deletion with confirmation
     */
    public DeleteAccountPage confirmDeletion(String confirmationText) {
        CONFIRMATION_INPUT.setValue(confirmationText);
        CONFIRM_DELETION_BUTTON.click();
        return this;
    }

    /**
     * Complete account deletion with correct confirmation text
     */
    public DeleteAccountPage confirmDeletion() {
        return confirmDeletion("DELETE");
    }

    /**
     * Cancel the deletion process
     */
    public DeleteAccountPage cancelDeletion() {
        CANCEL_BUTTON.click();
        // Wait for modal to disappear
        DELETE_CONFIRMATION_MODAL.should(disappear);
        return this;
    }

    /**
     * Get success message after deletion
     */
    public String getSuccessMessage() {
        return GLOBAL_MESSAGE.shouldBe(visible).text();
    }

    /**
     * Get error message
     */
    public String getErrorMessage() {
        return GLOBAL_ERROR.shouldBe(visible).text();
    }

    /**
     * Check if deletion was successful
     */
    public boolean isDeletionSuccessful() {
        return GLOBAL_MESSAGE.has(cssClass("alert-success"));
    }

    /**
     * Check if there's an error message
     */
    public boolean hasError() {
        return GLOBAL_ERROR.exists() && GLOBAL_ERROR.isDisplayed();
    }

    /**
     * Check if the confirmation modal is visible
     */
    public boolean isConfirmationModalVisible() {
        return DELETE_CONFIRMATION_MODAL.has(cssClass("show"));
    }

    /**
     * Check if the delete form is hidden (after successful deletion)
     */
    public boolean isDeleteFormHidden() {
        SelenideElement form = Selenide.$x("//form[@id='deleteAccountForm']");
        return form.has(cssClass("d-none"));
    }

    /**
     * Wait for page to load completely
     */
    public DeleteAccountPage waitForPageLoad() {
        DELETE_ACCOUNT_BUTTON.should(appear);
        return this;
    }

    /**
     * Clear confirmation input
     */
    public DeleteAccountPage clearConfirmationInput() {
        CONFIRMATION_INPUT.clear();
        return this;
    }

    /**
     * Get the current confirmation input value
     */
    public String getConfirmationInputValue() {
        return CONFIRMATION_INPUT.getValue();
    }
}