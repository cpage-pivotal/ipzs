import { Component, input, output, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from '../../../shared/material/material.module';
import { TranslationService } from '../../../services/translation.service';

@Component({
  selector: 'app-date-selector',
  standalone: true,
  imports: [CommonModule, FormsModule, MaterialModule],
  templateUrl: './date-selector.component.html',
  styleUrls: ['./date-selector.component.scss']
})
export class DateSelectorComponent {
  currentDate = input.required<Date>();
  dateChange = output<Date>();

  selectedDate = signal<Date>(new Date());

  constructor(public translationService: TranslationService) {
    effect(() => {
      this.selectedDate.set(this.currentDate());
    });
  }

  onDateChange(date: Date): void {
    if (date) {
      this.selectedDate.set(date);
      this.dateChange.emit(date);
    }
  }
}