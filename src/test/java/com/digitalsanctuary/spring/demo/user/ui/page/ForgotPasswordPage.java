package com.digitalsanctuary.spring.demo.user.ui.page;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import org.openqa.selenium.By;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.digitalsanctuary.spring.demo.user.ui.BaseUiTest;

public class ForgotPasswordPage extends BaseUiTest {
    private final SelenideElement EMAIL_FIELD = $(By.id("email"));
    private final SelenideElement SUBMIT_BTN = $x("//button");

    public ForgotPasswordPage(String url) {
        Selenide.open(url);
    }

    public ForgotPasswordPage fillEmail(String email) {
        EMAIL_FIELD.setValue(email);
        return this;
    }

    public SuccessResetPasswordPage clickSubmitBtn() {
        SUBMIT_BTN.click();
        return new SuccessResetPasswordPage();
    }

}
