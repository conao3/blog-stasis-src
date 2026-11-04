{
  description = "blog-clojure-src";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = inputs @ {
    flake-parts,
    treefmt-nix,
    ...
  }:
    flake-parts.lib.mkFlake {inherit inputs;} {
      systems = ["x86_64-linux" "aarch64-darwin"];
      imports = [
        treefmt-nix.flakeModule
      ];

      perSystem = {pkgs, ...}: let
        jdk = pkgs.jdk25;
        nodejs = pkgs.nodejs_26;
        clojure = pkgs.clojure.override {inherit jdk;};
      in {
        devShells.default = pkgs.mkShell {
          packages = [
            jdk
            clojure
            nodejs
          ];
        };

        treefmt = {
          projectRootFile = "flake.nix";
          programs.alejandra.enable = true;
        };
      };
    };
}
