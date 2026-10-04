import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { marked } from 'marked';

// [innerHTML] runs through Angular's sanitizer, which strips scripts, event handlers and javascript: URLs.
@Component({
  selector: 'app-markdown',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="prose" [innerHTML]="html()"></div>`,
})
export class Markdown {
  readonly source = input.required<string>();

  protected readonly html = computed(
    () => marked.parse(this.source(), { async: false, gfm: true, breaks: true }) as string,
  );
}
