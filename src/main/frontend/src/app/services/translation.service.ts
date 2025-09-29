import { Injectable, signal } from '@angular/core';

export type Language = 'en' | 'it';

export interface TranslationKey {
  en: string;
  it: string;
}

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private currentLanguage = signal<Language>('it');

  private translations: Record<string, TranslationKey> = {
    // App Navigation
    'nav.chat': { en: 'Chat', it: 'Chat' },
    'nav.documents': { en: 'Documents', it: 'Documenti' },
    'app.title': { en: 'IPZS Legislative Assistant', it: 'Assistente Legislativo IPZS' },
    'language.toggle': { en: 'Switch to Italian', it: 'Switch to English' },

    // Document Management
    'documents.title': { en: 'Legislative Document Management', it: 'Gestione Documenti Legislativi' },
    'documents.subtitle': { en: 'Sample Data Generation & Vector Store Management', it: 'Generazione Dati di Esempio e Gestione Vector Store' },
    'documents.generate': { en: 'Generate Sample Documents', it: 'Genera Documenti di Esempio' },
    'documents.refresh': { en: 'Refresh List', it: 'Aggiorna Elenco' },
    'documents.generating': { en: 'Generating...', it: 'Generazione...' },
    'documents.search': { en: 'Search documents', it: 'Cerca documenti' },
    'documents.search.placeholder': { en: 'Enter search terms...', it: 'Inserisci termini di ricerca...' },
    'documents.contextDate': { en: 'Context Date', it: 'Data Contesto' },
    'documents.contextDate.placeholder': { en: 'Select context date', it: 'Seleziona data contesto' },
    'documents.loading': { en: 'Loading documents...', it: 'Caricamento documenti...' },
    'documents.noResults': { en: 'No documents found', it: 'Nessun documento trovato' },
    'documents.noResults.subtitle': { en: 'Try adjusting your search criteria or generate sample documents.', it: 'Prova a modificare i criteri di ricerca o genera documenti di esempio.' },

    // Statistics
    'stats.total': { en: 'Total Documents', it: 'Documenti Totali' },
    'stats.current': { en: 'Currently Effective', it: 'Attualmente Efficaci' },
    'stats.superseded': { en: 'Superseded Documents', it: 'Documenti Superati' },

    // Document Details
    'document.id': { en: 'Document ID', it: 'ID Documento' },
    'document.number': { en: 'Document Number', it: 'Numero Documento' },
    'document.authority': { en: 'Issuing Authority', it: 'Autorità Emittente' },
    'document.publication': { en: 'Publication Date', it: 'Data Pubblicazione' },
    'document.effective': { en: 'Effective Date', it: 'Data Entrata in Vigore' },
    'document.expiration': { en: 'Expiration Date', it: 'Data Scadenza' },
    'document.provisions': { en: 'Key Provisions', it: 'Disposizioni Principali' },
    'document.supersedes': { en: 'Supersedes', it: 'Supera' },
    'document.supersededBy': { en: 'Superseded By', it: 'Superato Da' },
    'document.effective.short': { en: 'Effective', it: 'Efficace' },

    // Chat Component
    'chat.title': { en: 'IPZS Legislative Assistant', it: 'Assistente Legislativo IPZS' },
    'chat.subtitle': { en: 'Intelligent legislative consultation system', it: 'Sistema di consultazione legislativa intelligente' },
    'chat.dateLabel': { en: 'Reference Date', it: 'Data di riferimento' },
    'chat.dateInfo': { en: 'Responses will be based on legislation valid on this date', it: 'Le risposte saranno basate sulla legislazione valida a questa data' },
    'chat.welcome.title': { en: 'Welcome to the IPZS Legislative Assistant', it: 'Benvenuto nell\'Assistente Legislativo IPZS' },
    'chat.welcome.message': { en: 'You can ask me questions about Italian legislation. Select a reference date and start chatting!', it: 'Puoi farmi domande sulla legislazione italiana. Seleziona una data di riferimento e inizia a chattare!' },

    // Chat Input
    'chat.input.label': { en: 'Write your question...', it: 'Scrivi la tua domanda...' },
    'chat.input.placeholder': { en: 'E.g.: What consumer protections do I have if an airline delays my baggage?', it: 'Es: Quali tutele per i consumatori ho se una compagnia aerea ritarda il mio bagaglio?' },
    'chat.input.send': { en: 'Send', it: 'Invia' }
  };

  constructor() {
    // Load saved language preference
    const savedLang = localStorage.getItem('language') as Language;
    if (savedLang && (savedLang === 'en' || savedLang === 'it')) {
      this.currentLanguage.set(savedLang);
    }
  }

  getCurrentLanguage() {
    return this.currentLanguage;
  }

  setLanguage(language: Language) {
    this.currentLanguage.set(language);
    localStorage.setItem('language', language);
  }

  toggleLanguage() {
    const current = this.currentLanguage();
    const newLang: Language = current === 'en' ? 'it' : 'en';
    this.setLanguage(newLang);
  }

  translate(key: string): string {
    const translation = this.translations[key];
    if (!translation) {
      console.warn(`Translation missing for key: ${key}`);
      return key;
    }
    return translation[this.currentLanguage()];
  }

  // Helper method for template usage
  t(key: string): string {
    return this.translate(key);
  }
}
