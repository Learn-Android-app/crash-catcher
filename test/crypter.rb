# encoding: UTF-8
=begin
Created on 2013-02-25

@author: Nikolay Moskvin <moskvin@netcook.org>

=end

require 'openssl'
require 'optparse'

options = {}
OptionParser.new do |opts|
    opts.banner = "Usage: crypter.rb [options]"
    opts.on("-s", "--salt [SALT]", String, "The salt") do |s|
        options[:salt] = s
    end
    opts.on("-p", "--password [PASSWORD]", String, "The password") do |s|
        options[:password] = s
    end
    opts.on("-o", "--output [FILE]", String, "The output file") do |o|
        options[:output] = o
    end
end.parse!

salt = options[:salt]
password = options[:password]
output_file_name = options[:output]
file_name = ARGV[0]

# Android application password and salt
salt ||= File.read("salt")[0..-2]
password ||= File.read("password")[0..-2]
file_name ||= 'logcat.txt'
output_file_name ||= 'logcat_decrypt.txt'

puts "Salt: #{salt}, Password: #{password}\n"

is_read_msg = false
d = OpenSSL::Cipher.new("AES-256-CBC")
d.decrypt
key = OpenSSL::PKCS5.pbkdf2_hmac_sha1(password, salt, 1024, d.key_len)
d.key = key


File.readlines(file_name).each do |line|
    if is_read_msg
        d.iv = line.scan(/../).map{|b|b.hex}.pack('c*')
    else
        File.open(output_file_name, 'w') do |result|
            result.puts d.update(line.scan(/../).map{|b|b.hex}.pack('c*')) << d.final
        end
    end
    is_read_msg = true
end



