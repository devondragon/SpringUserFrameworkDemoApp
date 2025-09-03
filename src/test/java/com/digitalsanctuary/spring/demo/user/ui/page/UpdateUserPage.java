package com.digitalsanctuary.spring.demo.user.ui.page;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.visible;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

/**
 * Page object for update user profile page
 */
public class UpdateUserPage {

    private final SelenideElement FIRST_NAME_FIELD = Selenide.$x("//input[@id='firstName']");
    private final SelenideElement LAST_NAME_FIELD = Selenide.$x("//input[@id='lastName']");
    private final SelenideElement UPDATE_BUTTON = Selenide.$x("//button[@type='submit']");
    private final SelenideElement GLOBAL_MESSAGE = Selenide.$x("//div[@id='globalMessage']");
    private final SelenideElement CHANGE_PASSWORD_LINK = Selenide.$x("//a[@href='/user/update-password.html']");
    private final SelenideElement DELETE_ACCOUNT_LINK = Selenide.$x("//a[@href='/user/delete-account.html']");

    public UpdateUserPage(String url) {
        Selenide.open(url);
    }

    public UpdateUserPage() {
        // For navigation to this page from other pages
    }

    /**
     * Update user profile information
     */
    public UpdateUserPage updateProfile(String firstName, String lastName) {
        FIRST_NAME_FIELD.clear();
        FIRST_NAME_FIELD.setValue(firstName);
        LAST_NAME_FIELD.clear();
        LAST_NAME_FIELD.setValue(lastName);
        UPDATE_BUTTON.click();

        // Wait for success message to appear
        GLOBAL_MESSAGE.should(appear);
        return this;
    }

    /**
     * Get the success/error message displayed after update
     */
    public String getMessage() {
        return GLOBAL_MESSAGE.shouldBe(visible).text();
    }

    /**
     * Check if update was successful
     */
    public boolean isUpdateSuccessful() {
        return GLOBAL_MESSAGE.has(cssClass("alert-success"));
    }

    /**
     * Navigate to change password page
     */
    public UpdatePasswordPage goToChangePassword() {
        CHANGE_PASSWORD_LINK.click();
        return new UpdatePasswordPage();
    }

    /**
     * Navigate to delete account page
     */
    public DeleteAccountPage goToDeleteAccount() {
        DELETE_ACCOUNT_LINK.click();
        return new DeleteAccountPage();
    }

    /**
     * Get current first name value
     */
    public String getFirstName() {
        return FIRST_NAME_FIELD.getValue();
    }

    /**
     * Get current last name value
     */
    public String getLastName() {
        return LAST_NAME_FIELD.getValue();
    }

    /**
     * Wait for page to load completely
     */
    public UpdateUserPage waitForPageLoad() {
        FIRST_NAME_FIELD.should(appear);
        LAST_NAME_FIELD.should(appear);
        UPDATE_BUTTON.should(appear);
        return this;
    }
}
