import { Component, computed, input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTrophy } from '../../icons';

/** Respaldo por si la respuesta no trae `message`; el backend lo envía siempre que capApplied = true. */
const CAP_MESSAGE = '¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)';

/** HU4: alerta persistente cuando capApplied = true. */
@Component({
  selector: 'app-cap-alert',
  imports: [FaIconComponent],
  template: `
    <div
      class="alert alert-warning border-warning border-2 d-flex align-items-center gap-3 shadow-sm"
      role="alert"
    >
      <fa-icon [icon]="faTrophy" size="2x" class="text-warning-emphasis" />
      <strong>{{ text() }}</strong>
    </div>
  `,
})
export class CapAlert {
  readonly message = input<string | null>(null);
  protected readonly text = computed(() => this.message() || CAP_MESSAGE);
  protected readonly faTrophy = faTrophy;
}
