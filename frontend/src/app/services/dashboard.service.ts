import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserDashboardDto } from '../models/dtos/UserDashboardDto';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly apiUrl = `${environment.mainApiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<UserDashboardDto> {
    return this.http.get<UserDashboardDto>(`${this.apiUrl}/me`);
  }
}
