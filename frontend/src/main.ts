import { bootstrapApplication } from '@angular/platform-browser';
import { loadRuntimeEnvironment } from './environments/environment.runtime';

// Load/validate public configuration before importing any application providers.
loadRuntimeEnvironment()
  .then(async () => {
    const [{ appConfig }, { AppComponent }] = await Promise.all([
      import('./app/app.config'),
      import('./app/app.component'),
    ]);
    await bootstrapApplication(AppComponent, appConfig);
  })
  .catch(() => {
    // Do not reveal configuration details to the browser or use any fallback.
    document.body.textContent = 'Configuration SIXPAY indisponible. Contactez votre administrateur.';
  });
