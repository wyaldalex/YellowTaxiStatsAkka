FROM openjdk:11

# Copy the application files to the container
WORKDIR /app
COPY target/universal/YellowTaxiStatsAkka-1.0.0-SNAPSHOT.zip /app
RUN unzip YellowTaxiStatsAkka-1.0.0-SNAPSHOT.zip && rm YellowTaxiStatsAkka-1.0.0-SNAPSHOT.zip

# Expose the application port
EXPOSE 10001

# Start the application
CMD ["/app/YellowTaxiStatsAkka-1.0.0-SNAPSHOT/bin/yellowtaxistatsakka"]