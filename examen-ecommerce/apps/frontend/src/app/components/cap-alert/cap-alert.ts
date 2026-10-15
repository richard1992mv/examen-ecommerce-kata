import { Component, computed, input } from '@angular/core';
import { CAP_MESSAGE } from '../../services/http-error.util';

/** HU4: alerta persistente cuando capApplied = true. */
@Component({
  selector: 'app-cap-alert',
  template: `<div class="cap-alert" role="alert">{{ text() }}</div>`,
})
export class CapAlert {
  readonly message = input<string | null>(null);
  protected readonly text = computed(() => this.message() || CAP_MESSAGE);
}
