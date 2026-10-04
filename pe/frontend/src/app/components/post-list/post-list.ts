import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Post } from '../../models/post';

@Component({
  selector: 'app-post-list',
  imports: [DatePipe],
  templateUrl: './post-list.html',
  styleUrl: './post-list.css',
})
export class PostList {
  @Input() posts: Post[] = [];
  @Input() moderator = false;
  @Output() edit = new EventEmitter<Post>();
  @Output() remove = new EventEmitter<Post>();
}
