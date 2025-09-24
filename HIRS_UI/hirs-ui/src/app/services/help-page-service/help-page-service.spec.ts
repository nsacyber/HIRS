import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { HelpPageService } from './help-page-service';

describe('HelpPageService', () => {
  let service: HelpPageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(HelpPageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
