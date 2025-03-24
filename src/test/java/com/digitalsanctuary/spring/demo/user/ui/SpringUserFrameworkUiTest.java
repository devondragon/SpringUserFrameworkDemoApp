package com.digitalsanctuary.spring.demo.user.ui;


import static com.digitalsanctuary.spring.demo.user.ui.data.UiTestData.ACCOUNT_EXIST_ERROR_MESSAGE;
import static com.digitalsanctuary.spring.demo.user.ui.data.UiTestData.SUCCESS_RESET_PASSWORD_MESSAGE;
import static com.digitalsanctuary.spring.demo.user.ui.data.UiTestData.SUCCESS_SING_UP_MESSAGE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.digitalsanctuary.spring.demo.user.ui.data.UiTestData;
import com.digitalsanctuary.spring.demo.user.ui.page.ForgotPasswordPage;
import com.digitalsanctuary.spring.demo.user.ui.page.LoginPage;
import com.digitalsanctuary.spring.demo.user.ui.page.LoginSuccessPage;
import com.digitalsanctuary.spring.demo.user.ui.page.RegisterPage;
import com.digitalsanctuary.spring.demo.user.ui.page.SuccessRegisterPage;
import com.digitalsanctuary.spring.demo.user.ui.page.SuccessResetPasswordPage;
import com.digitalsanctuary.spring.user.dto.UserDto;
import com.digitalsanctuary.spring.user.jdbc.Jdbc;

@Tag("ui")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class SpringUserFrameworkUiTest extends BaseUiTest {

    private static final String URI = "http://localhost:8082/";

    private static final UserDto testUser = UiTestData.getUserDto();

    @Value("${test.browser}")
    private String browser;

    @BeforeAll
    public void setBrowser() {
        switch (browser) {
            case "chrome" -> super.setDriver(Driver.CHROME);
            case "opera" -> super.setDriver(Driver.OPERA);
            case "firefox" -> super.setDriver(Driver.FIREFOX);
            case "edge" -> super.setDriver(Driver.EDGE);
        }
    }

    {
        super.setDriver(Driver.CHROME);
    }

    @AfterEach
    public void deleteTestUser() {
        Jdbc.deleteTestUser(testUser);
    }

    @Test
    public void successSignUp() {
        SuccessRegisterPage registerPage = new RegisterPage(URI + "user/register.html").signUp(testUser.getFirstName(), testUser.getLastName(),
                testUser.getEmail(), testUser.getPassword(), testUser.getMatchingPassword());
        String actualMessage = registerPage.message();
        Assertions.assertEquals(SUCCESS_SING_UP_MESSAGE, actualMessage);
    }

    @Test
    public void userAlreadyExistSignUp() {
        Jdbc.saveTestUser(testUser);
        RegisterPage registerPage = new RegisterPage(URI + "user/register.html");
        registerPage.signUp(testUser.getFirstName(), testUser.getLastName(), testUser.getEmail(), testUser.getPassword(),
                testUser.getMatchingPassword());
        String actualMessage = registerPage.accountExistErrorMessage();
        Assertions.assertEquals(ACCOUNT_EXIST_ERROR_MESSAGE, actualMessage);
    }

    /**
     * checks that welcome message in success login page contains username
     */
    @Test
    public void successSignIn() {
        Jdbc.saveTestUser(testUser);
        LoginPage loginPage = new LoginPage(URI + "user/login.html");
        LoginSuccessPage loginSuccessPage = loginPage.signIn(testUser.getEmail(), testUser.getPassword());
        String welcomeMessage = loginSuccessPage.welcomeMessage();
        String firstName = testUser.getFirstName();
        Assertions.assertTrue(welcomeMessage.contains(firstName));
    }

    @Test
    public void successResetPassword() {
        Jdbc.saveTestUser(testUser);
        ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(URI + "user/forgot-password.html");
        SuccessResetPasswordPage successResetPasswordPage = forgotPasswordPage.fillEmail(testUser.getEmail()).clickSubmitBtn();
        String actualMessage = successResetPasswordPage.message();
        Assertions.assertEquals(SUCCESS_RESET_PASSWORD_MESSAGE, actualMessage);

    }
}
