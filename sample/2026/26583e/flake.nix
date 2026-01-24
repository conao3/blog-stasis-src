{
  description = "Minimal NixOS VM with X11";

  inputs.nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }: {
    nixosConfigurations.minimal-vm = nixpkgs.lib.nixosSystem {
      system = "x86_64-linux";
      modules = [({ modulesPath, pkgs, ... }: {
        imports = [ (modulesPath + "/virtualisation/qemu-vm.nix") ];

        services.xserver.enable = true;
        services.xserver.desktopManager.xfce.enable = true;

        users.users.demo = {
          isNormalUser = true;
          extraGroups = [ "wheel" ];
          initialPassword = "demo";
        };

        environment.systemPackages = with pkgs; [ git emacs ];

        system.stateVersion = "24.11";
      })];
    };

    packages.x86_64-linux.default = self.nixosConfigurations.minimal-vm.config.system.build.vm;
  };
}
