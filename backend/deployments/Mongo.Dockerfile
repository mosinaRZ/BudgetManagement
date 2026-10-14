FROM mongo:7

# The keyfile is generated at first start (see mongo-entrypoint.sh); nothing secret is baked into the image.
COPY mongo-entrypoint.sh /usr/local/bin/mongo-entrypoint.sh
RUN chmod +x /usr/local/bin/mongo-entrypoint.sh && mkdir -p /keyfile

ENTRYPOINT ["mongo-entrypoint.sh"]
CMD ["mongod"]
