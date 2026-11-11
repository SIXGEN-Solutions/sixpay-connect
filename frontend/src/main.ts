/**
 * Bootstrap boundary: do not statically import Angular or application providers.
 * Load and validate the public runtime configuration before evaluating the app.
 * The image remains identical across deployments; no mock/config fallback.
 */
async function startSixpay(): Promise<void> {
  const { loadRuntimeEnvironment } = await import('./environments/environment.runtime');
  await loadRuntimeEnvironment();

  // Keep the application dependency graph out of the initial static imports.
  // Sequential imports make initialization order explicit and reviewable.
  const { bootstrapApplication } = await import('@angular/platform-browser');
  const { appConfig } = await import('./app/app.config');
  const { AppComponent } = await import('./app/app.component');
  await bootstrapApplication(AppComponent, appConfig);
}

void startSixpay().catch((error: unknown) => {
  // Diagnostics only: do not log the JSON payload, credentials or tokens.
  // Keep the user-facing failure closed rather than falling back to mocks.
  console.error('SIXPAY startup failed:', error);
  document.body.textContent = 'Configuration SIXPAY indisponible. Contactez votre administrateur.';
});
