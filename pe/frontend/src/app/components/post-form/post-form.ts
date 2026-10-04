import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Post, PostRequest } from '../../models/post';

@Component({
  selector: 'app-post-form',
  imports: [FormsModule],
  templateUrl: './post-form.html',
  styleUrl: './post-form.css',
})
export class PostForm implements OnChanges {
  @Input() post: Post | null = null;
  @Input() authorName = '';
  @Input() saving = false;
  @Output() save = new EventEmitter<PostRequest>();
  @Output() cancel = new EventEmitter<void>();

  title = '';
  content = '';
  author = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['post']) {
      this.title = this.post?.title ?? '';
      this.content = this.post?.content ?? '';
      this.author = this.post?.author ?? '';
    }
    if (changes['authorName'] && !this.post) {
      this.author = this.authorName;
    }
  }

  submit(): void {
    this.save.emit({
      title: this.title.trim(),
      content: this.content.trim(),
      author: this.author.trim(),
    });
  }

  reset(): void {
    this.title = '';
    this.content = '';
    this.author = this.authorName;
  }
}
