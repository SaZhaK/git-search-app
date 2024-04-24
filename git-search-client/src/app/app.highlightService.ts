import { Injectable } from '@angular/core';
import hljs from 'highlight.js';

/*
* Service for highlighting code lines
*/
@Injectable({
  providedIn: 'root'
})
export class AppHighlightService {
  searchRequest = null;

  /*
  * Highlights code lines.
  * Uses highlightJs library for code syntax highlighting. See https://highlightjs.org/ for additional info.
  * Also highlights user's search request.
  */
  highlightCode(line, linePath, searchedString): string {
    this.searchRequest = searchedString;
    let withHighlightedSyntax = null;
    if (this.isComment(line)) {
      withHighlightedSyntax = this.highlightComment(line);
    } else {
      const extension = linePath.split('.').pop();
      withHighlightedSyntax = this.highlightSyntax(line, extension);
    }
    const withHighlightedSearchedString = this.highlightSearchedString(withHighlightedSyntax);
    return withHighlightedSearchedString;
  }

  private highlightSyntax(line, language): string {
    if (language === 'iml') {
      language = 'xml';
    }
    const isLanguagePresent = hljs.getLanguage(language) !== undefined;

    if (isLanguagePresent) {
      return hljs.highlight(line, {language}).value;
    } else {
      return line;
    }
  }

  // Workaround for comments as highlightJs is not able to detect parts of multiline comments separately
  private isComment(line): boolean {
    const trimmedLine = line.trimLeft();
    return trimmedLine.startsWith('/*')
      || trimmedLine.startsWith('*')
      || trimmedLine.startsWith('/*');
  }

  private highlightComment(line): string {
    return '<span class="hljs-comment">' + line + '</span>';
  }

  highlightSearchedString(htmlString): string {
    const container = document.createElement('div');
    container.innerHTML = htmlString;
    this.traverseElements(container);
    return container.innerHTML;
  }

  private traverseElements(el): void {
    if (!/^(script|style)$/.test(el.tagName)) {
      let child = el.lastChild;
      while (child) {
        if (child.nodeType === Node.ELEMENT_NODE) {
          this.traverseElements(child);
        } else if (child.nodeType === Node.TEXT_NODE) {
          this.replaceNode(child);
        }
        child = child.previousSibling;
      }
    }
  }

  private replaceNode(textNode): void {
    const escaped = this.searchRequest.replace(/['".*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(escaped, 'ig');

    const nodeData = textNode.data;
    const parentNode = textNode.parentNode;

    if (nodeData.match(regex)) {
      let remainingString = nodeData;
      let searchedStringStartIdx = remainingString.search(regex);

      const newTextNode = document.createElement('span');
      do {
        const beforeSearchedString = remainingString.substring(0, searchedStringStartIdx);
        const searchedStringLength = searchedStringStartIdx + this.searchRequest.length;
        const searchedString = remainingString.substring(searchedStringStartIdx, searchedStringLength);

        const beforeSearchedSpan = document.createElement('span');
        beforeSearchedSpan.innerText = beforeSearchedString;

        const searchedSpan = document.createElement('span');
        searchedSpan.setAttribute('class', 'searchedString');
        searchedSpan.innerText = searchedString;

        newTextNode.appendChild(beforeSearchedSpan);
        newTextNode.appendChild(searchedSpan);

        remainingString = remainingString.substring(searchedStringLength);
        searchedStringStartIdx = remainingString.search(regex);
      } while (searchedStringStartIdx !== -1);

      const remainingSpan = document.createElement('span');
      remainingSpan.innerText = remainingString;
      newTextNode.appendChild(remainingSpan);

      parentNode.replaceChild(newTextNode, textNode);
    } else {
      for (let idx = 1; idx < this.searchRequest.length; idx++) {
        const startOfRequest = this.searchRequest.substring(0, this.searchRequest.length - idx);
        if (nodeData.toLowerCase().endsWith(startOfRequest.toLowerCase())) {
          let remainingOfRequest = this.searchRequest.substring(this.searchRequest.length - idx);

          let curNode = textNode;
          if (!curNode.innerText) {
            const span = document.createElement('span');
            span.innerText = curNode.data;

            parentNode.replaceChild(span, curNode);
            curNode = span;
          }

          const nodes = [];
          const firstChunk = {
            data: startOfRequest,
            node: curNode
          };
          nodes.push(firstChunk);

          let readLast = false;
          while (!readLast && curNode.nextSibling != null) {
            let nextSibling = curNode.nextSibling;

            if (!nextSibling.innerText) {
              const span = document.createElement('span');
              span.innerText = nextSibling.data;

              parentNode.replaceChild(span, nextSibling);
              nextSibling = span;
            }
            const nextNodeData = nextSibling.innerText;

            const isLastSelectedChunk = !readLast
              && nextNodeData.length >= remainingOfRequest.length
              && nextNodeData.toLowerCase().startsWith(remainingOfRequest.toLowerCase());

            const isIntermediateChunk = !readLast
              && nextNodeData.length < remainingOfRequest.length
              && nextNodeData.toLowerCase() === remainingOfRequest.toLowerCase().substring(0, nextNodeData.length);

            if (isLastSelectedChunk) {
              const chunk = {
                data: remainingOfRequest,
                node: curNode.nextSibling
              };
              nodes.push(chunk);
              readLast = true;
            } else if (isIntermediateChunk) {
              const data = remainingOfRequest.substring(0, nextNodeData.length);
              const chunk = {
                data,
                node: curNode.nextSibling
              };
              nodes.push(chunk);
              remainingOfRequest = remainingOfRequest.substring(nextNodeData.length);
              curNode = nextSibling;
            } else {
              break;
            }
          }

          if (readLast) {
            const firstElement = nodes[0];
            const firstNode = firstElement.node;
            const firstData = firstElement.data;
            const firstNodeData = firstNode.innerText;
            firstNode.innerText = null;

            const beforeSearchedString = firstNodeData.substring(0, firstNodeData.length - firstData.length);
            const beforeSearchedSpan = document.createElement('span');
            beforeSearchedSpan.innerText = beforeSearchedString;

            const searchedSpan = document.createElement('span');
            searchedSpan.setAttribute('class', 'searchedStringLeft');
            searchedSpan.innerText = firstNodeData.substring(firstNodeData.length - firstData.length);

            firstNode.appendChild(beforeSearchedSpan);
            firstNode.appendChild(searchedSpan);

            for (let i = 1; i < nodes.length - 1; i++) {
              const secondElement = nodes[i];
              const secondNode = secondElement.node;
              const secondData = secondElement.data;
              const secondNodeData = secondNode.innerText;
              secondNode.innerText = null;

              const searchedSpan2 = document.createElement('span');
              searchedSpan2.setAttribute('class', 'searchedStringMiddle');
              searchedSpan2.innerText = secondNodeData.substring(secondNodeData.length - secondData.length);

              secondNode.appendChild(searchedSpan2);
            }

            const lastElement = nodes[nodes.length - 1];
            const lastNode = lastElement.node;
            const lastData = lastElement.data;
            const lastNodeData = lastNode.innerText;
            lastNode.innerText = null;

            const searchedSpan3 = document.createElement('span');
            searchedSpan3.setAttribute('class', 'searchedStringRight');
            searchedSpan3.innerText = lastNodeData.substring(0, lastData.length);

            const afterSearchedString = lastNodeData.substring(lastData.length);
            const afterSearchedSpan = document.createElement('span');
            afterSearchedSpan.innerText = afterSearchedString;

            lastNode.appendChild(searchedSpan3);
            lastNode.appendChild(afterSearchedSpan);
          }
        }
      }
    }
  }
}
