declare module '*.css';
declare module '*.scss';
declare module '*.svg';
declare module '*.png';

declare module 'react/jsx-runtime' {
  export * from 'react/jsx-runtime';
}

declare module 'antd/dist/reset.css';

interface File {
  readonly lastModified: number;
  readonly name: string;
  readonly webkitRelativePath: string;
}

declare const File: {
  prototype: File;
  new(fileBits: BlobPart[], fileName: string, options?: FilePropertyBag): File;
};