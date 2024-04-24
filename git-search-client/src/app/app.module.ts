import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';

import {Routes, RouterModule} from '@angular/router';

import { AppComponent } from './app.component';
import { SearchComponent } from './search.component';
import { HttpClientModule } from '@angular/common/http';

const appRoutes: Routes = [
    { path: '', component: SearchComponent }
];

@NgModule({
  declarations: [
    AppComponent,
    SearchComponent
  ],
  imports: [
    FormsModule,
    BrowserModule,
    HttpClientModule,
    RouterModule.forRoot(appRoutes)
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
