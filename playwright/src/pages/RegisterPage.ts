import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Registration page.
 */
export class RegisterPage extends BasePage {
  readonly path = '/user/register.html';

  // Form elements
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly termsCheckbox: Locator;
  readonly signUpButton: Locator;

  // Error displays
  readonly globalError: Locator;
  readonly existingAccountError: Locator;
  readonly firstNameError: Locator;
  readonly passwordError: Locator;

  // Password strength indicator
  readonly passwordStrength: Locator;
  readonly strengthLevel: Locator;

  // Links
  readonly loginLink: Locator;

  constructor(page: Page) {
    super(page);
    this.firstNameInput = page.locator('#firstName');
    this.lastNameInput = page.locator('#lastName');
    this.emailInput = page.locator('#email');
    this.passwordInput = page.locator('#password');
    this.confirmPasswordInput = page.locator('#matchPassword');
    this.termsCheckbox = page.locator('#terms');
    this.signUpButton = page.locator('#signUpButton');
    this.globalError = page.locator('#globalError');
    this.existingAccountError = page.locator('#existingAccountError');
    this.firstNameError = page.locator('#firstNameError');
    this.passwordError = page.locator('#passwordError');
    this.passwordStrength = page.locator('#password-strength');
    this.strengthLevel = page.locator('#strengthLevel');
    // Target the login link in the main content area (not navbar)
    this.loginLink = page.locator('#main_content a[href*="login"], .card a[href*="login"]').first();
  }

  /**
   * Fill in registration form.
   */
  async fillForm(
    firstName: string,
    lastName: string,
    email: string,
    password: string,
    confirmPassword?: string
  ): Promise<void> {
    await this.firstNameInput.fill(firstName);
    await this.lastNameInput.fill(lastName);
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.confirmPasswordInput.fill(confirmPassword || password);
  }

  /**
   * Accept terms and conditions.
   */
  async acceptTerms(): Promise<void> {
    await this.termsCheckbox.check();
  }

  /**
   * Submit the registration form.
   */
  async submit(): Promise<void> {
    await this.signUpButton.click();
  }

  /**
   * Complete registration flow.
   */
  async register(
    firstName: string,
    lastName: string,
    email: string,
    password: string
  ): Promise<void> {
    await this.goto();
    await this.fillForm(firstName, lastName, email, password);
    await this.acceptTerms();
    await this.submit();
  }

  /**
   * Register and wait for success page.
   * Handles both email verification enabled (registration-pending) and disabled (registration-complete) scenarios.
   */
  async registerAndWait(
    firstName: string,
    lastName: string,
    email: string,
    password: string
  ): Promise<void> {
    await this.register(firstName, lastName, email, password);
    // Wait for redirect to either pending verification or complete page (depends on email config)
    await this.page.waitForURL(/registration-(pending|complete)/, { timeout: 10000 });
  }

  /**
   * Check if global error is displayed.
   */
  async hasGlobalError(): Promise<boolean> {
    const isVisible = await this.globalError.isVisible();
    return isVisible;
  }

  /**
   * Get global error text.
   */
  async getGlobalErrorText(): Promise<string | null> {
    if (await this.hasGlobalError()) {
      return this.globalError.textContent();
    }
    return null;
  }

  /**
   * Check if existing account error is displayed.
   */
  async hasExistingAccountError(): Promise<boolean> {
    return (await this.existingAccountError.count() > 0) && (await this.existingAccountError.isVisible());
  }

  /**
   * Navigate to login page.
   */
  async goToLogin(): Promise<void> {
    await this.loginLink.click();
    await this.page.waitForURL('**/login**');
  }

  /**
   * Check if password strength indicator is visible.
   */
  async isPasswordStrengthVisible(): Promise<boolean> {
    return this.passwordStrength.isVisible();
  }

  /**
   * Get password strength level (width percentage).
   */
  async getPasswordStrengthLevel(): Promise<string | null> {
    if (await this.isPasswordStrengthVisible()) {
      return this.strengthLevel.getAttribute('style');
    }
    return null;
  }
}
