import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { SecurityUserDetail } from '../models/security-user-administration';
import { SecurityUserAdministrationService } from '../services/security-user-administration.service';

@Component({
  selector: 'sp-security-user-detail-page',
  imports: [DatePipe, ReactiveFormsModule, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      @if (user(); as data) {
        <sp-toolbar
          [title]="data.username"
          description="Administration de l’identité et de la sécurité utilisateur"
        />

        <sp-card title="Compte">
          <p>
            Username: <strong>{{ data.username }}</strong><br />
            Email: <strong>{{ data.email ?? '—' }}</strong><br />
            Statut: <strong>{{ data.status }}</strong>
          </p>
          @if (data.status !== 'DISABLED') {
            <button type="button" (click)="disableUser()">Désactiver l’utilisateur</button>
          }
        </sp-card>

        <sp-card title="Méthodes d’authentification">
          <p>Local: <strong>{{ data.localEnabled ? 'Activé' : 'Désactivé' }}</strong></p>
          <button type="button" (click)="toggleLocal()">
            {{ data.localEnabled ? 'Désactiver Local' : 'Activer Local' }}
          </button>
          <p>SSO/OIDC: <strong>{{ data.oidcLinked ? 'Lié' : 'Non lié' }}</strong></p>
        </sp-card>

        <sp-card title="Réinitialiser le mot de passe Local">
          <form [formGroup]="passwordForm" (ngSubmit)="resetPassword()" class="sp-form">
            <label>
              Nouveau mot de passe temporaire
              <input type="password" formControlName="newPassword" autocomplete="new-password" />
            </label>
            <button type="submit" [disabled]="passwordForm.invalid">Réinitialiser</button>
          </form>
        </sp-card>

        <sp-card title="Lier une identité OIDC">
          <form [formGroup]="oidcForm" (ngSubmit)="linkOidc()" class="sp-form">
            <label>Provider / issuer <input formControlName="provider" /></label>
            <label>Subject <input formControlName="providerSubject" /></label>
            <button type="submit" [disabled]="oidcForm.invalid">Lier l’identité</button>
          </form>

          @for (identity of data.identities; track identity.id) {
            <p>
              {{ identity.identityType }} — {{ identity.provider }}<br />
              Subject: {{ identity.providerSubject }} — {{ identity.status }}
              @if (identity.identityType === 'OIDC') {
                <button type="button" (click)="unlinkIdentity(identity.id)">Délier</button>
              }
            </p>
          }
        </sp-card>

        <sp-card title="Autorisation SIXPAY">
          <p>Rôles: {{ data.roles.join(', ') || '—' }}</p>
          <p>Permissions: {{ data.permissions.join(', ') || '—' }}</p>
        </sp-card>

        <sp-card title="Derniers événements de sécurité">
          @for (event of data.recentAuthenticationEvents; track event.occurredAt + event.eventType) {
            <p>
              <strong>{{ event.eventType }}</strong>
              — {{ event.occurredAt | date:'medium' }}
              @if (event.provider) { — {{ event.provider }} }
            </p>
          }
        </sp-card>
      }
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-form{display:grid;gap:var(--sp-space-3)}
    label{display:grid;gap:var(--sp-space-2)}
  `,
})
export class SecurityUserDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(SecurityUserAdministrationService);
  private readonly userId = this.route.snapshot.paramMap.get('userId')!;

  protected readonly user = signal<SecurityUserDetail | null>(null);

  protected readonly passwordForm = new FormGroup({
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(12)],
    }),
  });

  protected readonly oidcForm = new FormGroup({
    provider: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    providerSubject: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() { this.reload(); }

  protected toggleLocal(): void {
    const current = this.user();
    if (!current) return;
    this.service.setLocalEnabled(this.userId, !current.localEnabled)
      .subscribe((user) => this.user.set(user));
  }

  protected resetPassword(): void {
    if (this.passwordForm.invalid) return;
    const { newPassword } = this.passwordForm.getRawValue();
    this.service.resetLocalPassword(this.userId, newPassword)
      .subscribe((user) => {
        this.passwordForm.reset();
        this.user.set(user);
      });
  }

  protected linkOidc(): void {
    if (this.oidcForm.invalid) return;
    const value = this.oidcForm.getRawValue();
    this.service.linkOidcIdentity(this.userId, value.provider, value.providerSubject)
      .subscribe((user) => {
        this.oidcForm.reset();
        this.user.set(user);
      });
  }

  protected unlinkIdentity(identityId: string): void {
    this.service.unlinkIdentity(this.userId, identityId)
      .subscribe((user) => this.user.set(user));
  }

  protected disableUser(): void {
    this.service.disableUser(this.userId)
      .subscribe((user) => this.user.set(user));
  }

  private reload(): void {
    this.service.getUser(this.userId).subscribe((user) => this.user.set(user));
  }
}
