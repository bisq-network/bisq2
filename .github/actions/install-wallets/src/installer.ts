export interface Installer {
    programName: string
    install(): void;
}