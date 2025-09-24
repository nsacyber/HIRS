import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class HelpPageService {
  private http: HttpClient = inject(HttpClient);

   //todo
}
